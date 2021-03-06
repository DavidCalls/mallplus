<template>
  <div class="app-container">
    <el-card class="filter-container" shadow="never">
      <div>
        <i class="el-icon-search"></i>
        <span>筛选搜索</span>
        <el-button
          style="float: right"
          @click="searchUmsMemberLevelList()"
          type="primary"
          size="small"
        >查询结果</el-button>
        <el-button
          style="float:right;margin-right: 15px"
          @click="handleResetSearch()"
          size="small"
        >重置</el-button>
      </div>
      <div style="margin-top: 15px">
        <el-form :inline="true" :model="listQuery" size="small" label-width="140px">
          <el-form-item label="等级名称">
            <el-input style="width: 203px" v-model="listQuery.name" placeholder="按等级名称查询"></el-input>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    <el-card class="operate-container" shadow="never">
      <i class="el-icon-tickets"></i>
      <span>数据列表</span>
      <el-button class="btn-add" @click="addUmsMemberLevel()" size="mini">添加</el-button>
    </el-card>
    <div class="table-container">
      <el-table
        ref="umsMemberLevelTable"
        :data="list"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        v-loading="listLoading"
        border
      >
        <el-table-column type="selection" width="60" align="center"></el-table-column>

        <el-table-column prop="id" label="编号" align="center">
          <template slot-scope="scope">{{scope.row.id }}</template>
        </el-table-column>
        <el-table-column prop="name" label="等级名称" align="center">
          <template slot-scope="scope">{{scope.row.name }}</template>
        </el-table-column>
        <el-table-column prop="price" label="成为会员的价格" align="center">
          <template slot-scope="scope">{{scope.row.price }}</template>
        </el-table-column>
        <el-table-column prop="priviledgeMemberPrice" label="会员折扣(0.1-10)" align="center">
          <template slot-scope="scope">{{scope.row.priviledgeMemberPrice }}</template>
        </el-table-column>
        <el-table-column prop="state" label="状态" align="center">
            <!-- （0启用1禁用） -->
          <template slot-scope="scope">
            <!-- {{scope.row.state }} -->
            <div v-if="scope.row.state==='0'">启用</div>
            <div v-if="scope.row.state==='1'">禁用</div>
          </template>
        </el-table-column>
        <el-table-column prop="applyScene" label="适用场景" align="center">
            <!-- （0平台商品1平台积分商城） -->
          <template slot-scope="scope">
            <div v-if="scope.row.applyScene==='0'">平台商品</div>
            <div v-if="scope.row.applyScene==='1'">平台积分商城</div>
            <!-- {{scope.row.applyScene }} -->
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center">
          <template slot-scope="scope">
            <el-button size="mini" @click="handleUpdate(scope.$index, scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.$index, scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="batch-operate-container"></div>
    <div class="pagination-container">
      <el-pagination
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        layout="total, sizes,prev, pager, next,jumper"
        :page-size="listQuery.pageSize"
        :page-sizes="[10,15,50]"
        :current-page.sync="listQuery.pageNum"
        :total="total"
      ></el-pagination>
    </div>
  </div>
</template>
<script>
import { fetchList, deleteUmsMemberLevel } from "@/api/ums/umsMemberLevel";
import { formatDate } from "@/utils/date";
const defaultListQuery = {
  pageNum: 1,
  pageSize: 10,
  name: null,
};

export default {
  name: "umsMemberLevelList",
  data() {
    return {
      operates: [],
      operateType: null,
      listQuery: Object.assign({}, defaultListQuery),
      list: null,
      total: null,
      listLoading: true,
      multipleSelection: []
    };
  },
  created() {
    this.getList();
  },
  filters: {
    formatCreateTime(time) {
      let date = new Date(time);
      return formatDate(date, "yyyy-MM-dd hh:mm:ss");
    },

    formatStatus(value) {
      if (value === 1) {
        return "未开始";
      } else if (value === 2) {
        return "活动中";
      } else if (value === 3) {
        return "已结束";
      } else if (value === 4) {
        return "已失效";
      }
    }
  },
  methods: {
    handleResetSearch() {
      this.listQuery = Object.assign({}, defaultListQuery);
      this.getList();
    },
    getList() {
      this.listLoading = true;
      fetchList(this.listQuery).then(response => {
        this.listLoading = false;
        this.list = response.data.records;
        this.total = response.data.total;
        this.totalPage = response.data.pages;
        this.pageSize = response.data.size;
      });
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    handleUpdate(index, row) {
      this.$router.push({
        path: "/ums/updateUmsMemberLevel",
        query: { id: row.id }
      });
    },
    handleDelete(index, row) {
      this.$confirm("是否要删除该类型", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        deleteUmsMemberLevel(row.id).then(response => {
          this.$message({
            message: "删除成功",
            type: "success",
            duration: 1000
          });
          this.getList();
        });
      });
    },

    handleSizeChange(val) {
      this.listQuery.pageNum = 1;
      this.listQuery.pageSize = val;
      this.getList();
    },
    handleCurrentChange(val) {
      this.listQuery.pageNum = val;
      this.getList();
    },
    searchUmsMemberLevelList() {
      this.listQuery.pageNum = 1;
      this.getList();
    },
    handleBatchOperate() {
      if (this.multipleSelection < 1) {
        this.$message({
          message: "请选择一条记录",
          type: "warning",
          duration: 1000
        });
        return;
      }
      let showStatus = 0;
      if (this.operateType === "showUmsMemberLevel") {
        showStatus = 1;
      } else if (this.operateType === "hideUmsMemberLevel") {
        showStatus = 0;
      } else {
        this.$message({
          message: "请选择批量操作类型",
          type: "warning",
          duration: 1000
        });
        return;
      }
      let ids = [];
      for (let i = 0; i < this.multipleSelection.length; i++) {
        ids.push(this.multipleSelection[i].id);
      }
      let data = new URLSearchParams();
      data.append("ids", ids);
      data.append("showStatus", showStatus);
      updateShowStatus(data).then(response => {
        this.getList();
        this.$message({
          message: "修改成功",
          type: "success",
          duration: 1000
        });
      });
    },
    addUmsMemberLevel() {
      //手动将router,改成$router
      this.$router.push({ path: "/ums/addUmsMemberLevel" });
    }
  }
};
</script>
<style rel="stylesheet/scss" lang="scss" scoped>
</style>


