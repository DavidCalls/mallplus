<template>
	<view class="content">
		<view class="navbar" :style="{ position: headerPosition, top: headerTop }">
			<view class="nav-item" :class="{ current: filterIndex === 0 }" @click="tabClick(0)">综合排序</view>
			<view class="nav-item" :class="{ current: filterIndex === 1 }" @click="tabClick(1)">销量优先</view>
			<view class="nav-item" :class="{ current: filterIndex === 2 }" @click="tabClick(2)">
				<text>价格</text>
				<view class="p-box">
					<text :class="{ active: priceOrder === 1 && filterIndex === 2 }" class="yticon icon-shang"></text>
					<text :class="{ active: priceOrder === 2 && filterIndex === 2 }" class="yticon icon-shang xia"></text>
				</view>
			</view>
            <!-- <text class="cate-item yticon icon-fenlei1" @click="toggleCateMask('show')"></text> -->
			<text class="cate-item yticon icon-fenlei1" v-if="current == 0" @click="toShowMode('false')"></text>
			<text class="cate-item cgicon icon-vertical" v-else-if="current == 1" @click="toShowMode('true')"></text>
		</view>
		<!-- 表格图片显示 -->
		<view class="goods-list" v-show="current === 0">
			<view v-for="(item, index) in goodsList" :key="index" class="goods-item" @click="navToDetailPage(item)">
				<view class="image-wrapper"><image :src="item.pic" mode="aspectFill"></image></view>
				<text class="title clamp">{{ item.name }}</text>
				<view class="price-box" v-if="item.isFenxiao == 1">
					<text class="price">{{ item.price }}</text>
					<text>佣金 ¥{{ item.fenxiaoPrice }}</text>
				</view>
				<view class="price-box" v-if="item.isVip == 1">
					<text class="price">会员价{{ item.vipPrice }}</text>
					<text>商品价 ¥{{ item.price }}</text>
				</view>
				<view class="price-box" v-else-if="item.isFenxiao == 0 && item.isVip == 0">
					<text class="price">{{ item.price }}</text>
					<text>已售 {{ item.sale }}</text>
				</view>
			</view>
		</view>
		<!-- 列表显示 -->
		<view class="goods-list-vertical" v-show="current === 1">
			<view v-for="(item, index) in goodsList" :key="index" class="goods-item-vertical" @click="navToDetailPage(item)">
				<view class="image-wrapper-vertical"><image :src="item.pic" mode="aspectFill"></image></view>
				<view class="goods-list-right">
					<view class="goods-right-top">
						<text class="">{{ item.name }}</text>
					</view>
					<view class="goods-right-bottom">
						<text class="price">{{ item.price }}</text>
						<text class="sale">已售 {{ item.sale }}</text>
					</view>
				</view>
			</view>
		</view>
		<uni-load-more :status="loadingType"></uni-load-more>
    <view class="cate-mask" :class="cateMaskState === 0 ? 'none' : cateMaskState === 1 ? 'show' : ''" @click="toggleCateMask">
			<view class="cate-content" @click.stop.prevent="stopPrevent" @touchmove.stop.prevent="stopPrevent">
				<scroll-view scroll-y class="cate-list">
					<view v-for="item in cateList" :key="item.id">
						<view class="cate-item b-b two">{{ item.name }}</view>
						<view v-for="tItem in item.child" :key="tItem.id" class="cate-item b-b" :class="{ active: tItem.id == cateId }" @click="changeCate(tItem)">
							{{ tItem.name }}
						</view>
					</view>
				</scroll-view>
			</view>
		</view>
		<mallplusCopyright></mallplusCopyright>
	</view>
</template>

<script>
import mallplusCopyright from '@/components/mall-copyright/mallplusCopyright.vue';
import Api from '@/common/api';
import uniLoadMore from '@/components/uni-load-more/uni-load-more.vue';
export default {
	components: {
		mallplusCopyright,
		uniLoadMore
	},
	data() {
		return {
			cateMaskState: 0, //分类面板展开状态
			headerPosition: 'fixed',
			headerTop: '0px',
			keyword: '',
			loadingType: 'more', //加载更多状态
			filterIndex: 0,
			cateId: 0, //已选三级分类id
			productAttributeCategoryId: 0,
			pageNum: 1,
			isFenxiao: 0,
			isVip: 0,
			cid: null,
			priceOrder: 0, //1 价格从低到高 2价格从高到低
			cateList: [],
			goodsList: [],

			current: 0 // 1为列表模式
		};
	},

	onLoad(options) {
		// #ifdef H5
		this.headerTop = document.getElementsByTagName('uni-page-head')[0].offsetHeight + 'px';
		// #endif
		this.cateId = options.tagId;
	//	this.loadCateList(options.fid, options.sid);
		this.loadData();
	},
	onPageScroll(e) {
		//兼容iOS端下拉时顶部漂移
		if (e.scrollTop >= 0) {
			this.headerPosition = 'fixed';
		} else {
			this.headerPosition = 'absolute';
		}
	},
	//下拉刷新
	onPullDownRefresh() {
		this.pageNum = 1;
		this.loadData('refresh');
	},
	//加载更多
	onReachBottom() {
		this.pageNum = this.pageNum + 1;
		this.loadData();
	},
	methods: {
		toShowMode(mode) {
			if (this.current == 0) {
				this.current = 1;
			} else {
				this.current = 0;
			}
		},
//加载分类
		async loadCateList(fid, sid) {
			let params = {};
			let list = await Api.apiCall('get', Api.goods.typeList, params);
			let cateList = list.filter(item => item.pid == null);

			cateList.forEach(item => {
				let tempList = list.filter(val => val.pid == item.id);
				item.child = tempList;
			});
			this.cateList = cateList;
		},
		//加载商品 ，带下拉刷新和上滑加载
		async loadData(type = 'add', loading) {
			let params = { tags: this.cateId };

			let list = await Api.apiCall('get', Api.goods.tagGoodsList, params);
			let goodsList = list;

			//筛选，测试数据直接前端筛选了
			if (this.filterIndex === 1) {
				goodsList.sort((a, b) => b.sales - a.sales);
			}
			if (this.filterIndex === 2) {
				goodsList.sort((a, b) => {
					if (this.priceOrder == 1) {
						return a.price - b.price;
					}
					return b.price - a.price;
				});
			}

			this.goodsList = goodsList;
		},
		//筛选点击
		tabClick(index) {
			this.pageNum = 1;
			if (this.filterIndex === index && index !== 2) {
				return;
			}
			this.filterIndex = index;
			if (index === 2) {
				this.priceOrder = this.priceOrder === 1 ? 2 : 1;
			} else {
				this.priceOrder = 0;
			}
			uni.pageScrollTo({
				duration: 300,
				scrollTop: 0
			});
			this.loadData('refresh', 1);
		},
		//显示分类面板
		toggleCateMask(type) {
			let timer = type === 'show' ? 10 : 300;
			let state = type === 'show' ? 1 : 0;
			this.cateMaskState = 2;
			setTimeout(() => {
				this.cateMaskState = state;
			}, timer);
		},
		//分类点击
		changeCate(item) {
			this.pageNum = 1;
			this.cateId = item.id;
			this.toggleCateMask();
			uni.pageScrollTo({
				duration: 300,
				scrollTop: 0
			});
			this.loadData('refresh', 1);
		},
		//详情
		navToDetailPage(item) {
			//测试数据没有写id，用title代替
			let id = item.id;
			uni.navigateTo({
				url: `../../pagesA/product/product?id=${id}`
			});
		},
		stopPrevent() {}
	}
};
</script>

<style lang="scss">
@font-face {
	font-family: 'cgicon';
	font-weight: normal;
	font-style: normal;
	src: url('https://at.alicdn.com/t/font_1475801_65jj7c5ss9k.ttf') format('truetype');
}

.cgicon {
	font-family: 'cgicon' !important;
	font-size: 16px;
	font-style: normal;
	-webkit-font-smoothing: antialiased;
	-moz-osx-font-smoothing: grayscale;
}

.icon-vertical:before {
	content: '\e61b';
}

page,
.content {
	background: $page-color-base;
}
.content {
	padding-top: 96upx;
}

.navbar {
	position: fixed;
	left: 0;
	top: var(--window-top);
	display: flex;
	width: 100%;
	height: 80upx;
	background: #fff;
	box-shadow: 0 2upx 10upx rgba(0, 0, 0, 0.06);
	z-index: 10;
	.nav-item {
		flex: 1;
		display: flex;
		justify-content: center;
		align-items: center;
		height: 100%;
		font-size: 30upx;
		color: $font-color-dark;
		position: relative;
		&.current {
			color: $base-color;
			&:after {
				content: '';
				position: absolute;
				left: 50%;
				bottom: 0;
				transform: translateX(-50%);
				width: 120upx;
				height: 0;
				border-bottom: 4upx solid $base-color;
			}
		}
	}
	.p-box {
		display: flex;
		flex-direction: column;
		.yticon {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 30upx;
			height: 14upx;
			line-height: 1;
			margin-left: 4upx;
			font-size: 26upx;
			color: #888;
			&.active {
				color: $base-color;
			}
		}
		.xia {
			transform: scaleY(-1);
		}
	}
	.cate-item {
		display: flex;
		justify-content: center;
		align-items: center;
		height: 100%;
		width: 80upx;
		position: relative;
		font-size: 44upx;
		&:after {
			content: '';
			position: absolute;
			left: 0;
			top: 50%;
			transform: translateY(-50%);
			border-left: 1px solid #ddd;
			width: 0;
			height: 36upx;
		}
	}
}

/* 分类 */
.cate-mask {
	position: fixed;
	left: 0;
	top: var(--window-top);
	bottom: 0;
	width: 100%;
	background: rgba(0, 0, 0, 0);
	z-index: 95;
	transition: 0.3s;

	.cate-content {
		width: 630upx;
		height: 100%;
		background: #fff;
		float: right;
		transform: translateX(100%);
		transition: 0.3s;
	}
	&.none {
		display: none;
	}
	&.show {
		background: rgba(0, 0, 0, 0.4);

		.cate-content {
			transform: translateX(0);
		}
	}
}
.cate-list {
	display: flex;
	flex-direction: column;
	height: 100%;
	.cate-item {
		display: flex;
		align-items: center;
		height: 90upx;
		padding-left: 30upx;
		font-size: 28upx;
		color: #555;
		position: relative;
	}
	.two {
		height: 64upx;
		color: #303133;
		font-size: 30upx;
		background: #f8f8f8;
	}
	.active {
		color: $base-color;
	}
}

/* 商品列表 */
.goods-list {
	display: flex;
	flex-wrap: wrap;
	padding: 20upx 20upx 0;
	background: #fff;
	.goods-item {
		display: flex;
		flex-direction: column;
		width: 48%;
		padding-bottom: 40upx;
		&:nth-child(2n + 1) {
			margin-right: 4%;
		}
	}
	.image-wrapper {
		width: 100%;
		height: 330upx;
		border-radius: 3px;
		overflow: hidden;
		image {
			width: 100%;
			height: 100%;
			opacity: 1;
		}
	}
	.title {
		font-size: $font-lg;
		color: $font-color-dark;
		line-height: 80upx;
	}
	.price-box {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-right: 10upx;
		font-size: 24upx;
		color: $font-color-light;
	}
	.price {
		font-size: $font-lg;
		color: $uni-color-primary;
		line-height: 1;
		&:before {
			content: '￥';
			font-size: 26upx;
		}
	}
}
.goods-list-vertical {
	display: flex;
	flex-wrap: wrap;
	padding: 0upx 20upx;
	background: #fff;
	.goods-item-vertical {
		display: flex;
		flex-direction: row;
		height: 280upx;
		align-items: center;
		justify-content: center;
		border-bottom: 1upx solid #eeeeee;
		.image-wrapper-vertical {
			width: 240upx;
			height: 240upx;
			display: flex;
			align-items: center;
			justify-content: center;
			margin-right: 10upx;
			image {
				width: 240upx;
				height: 240upx;
			}
		}
		.goods-list-right {
			width: 470upx;
			.goods-right-top {
				height: 200upx;
				text {
					font-size: 32upx;
					text-overflow: -o-ellipsis-lastline;
					overflow: hidden;
					text-overflow: ellipsis;
					display: -webkit-box;
					-webkit-line-clamp: 2;
					-webkit-box-orient: vertical;
				}
			}
			.goods-right-bottom {
				display: flex;
				align-items: center;
				justify-content: space-between;
				.price {
					font-size: $font-lg;
					color: $uni-color-primary;
					line-height: 1;
					&:before {
						content: '￥';
						font-size: 26upx;
					}
				}
				.sale {
					color: $font-color-light;
					font-size: 28upx;
				}
			}
		}
	}
}
</style>
